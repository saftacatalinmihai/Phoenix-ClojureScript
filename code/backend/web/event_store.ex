defmodule Backend.EventStore do

  use GenServer

  def start_link do
    GenServer.start_link(__MODULE__, :ok, name: __MODULE__)
  end

  def get_events do 
    GenServer.call(__MODULE__, :get_events)
  end

  def get_pid do
    GenServer.call(__MODULE__, :get_pid)
  end

  def register_watcher(pid) do
    GenServer.call(__MODULE__, {:register_watcher, pid})
  end

  def init(:ok) do
    {:ok, %{:proc_events => [], :watchers => []}}
  end

  def handle_call(:get_events, _from, state) do
    {:reply, state[:proc_events], state}
  end

  def handle_call({:register_watcher, w}, _from, %{:proc_events => es, :watchers => ws}) do
    {:reply, :ok, %{:proc_events => es, :watchers => [w | ws]}}
  end

  def handle_call(:get_pid, _from, state) do
    {:reply, self(), state}
  end

  def handle_info(event, %{:proc_events => ev_list, :watchers => watchers}) do
    Enum.each(watchers, fn w -> send(to_string(w), event) end)
    {:noreply, %{:proc_events => [event | ev_list], :watchers => watchers}}
  end

end
