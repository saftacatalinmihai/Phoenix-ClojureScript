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

  def init(:ok) do
    # :erlang.trace(:processes, true, [:send, :receive])
    {:ok, []}
  end

  def handle_call(:get_events, _from, ev_list) do
    {:reply, ev_list, ev_list}
  end

  def handle_call(:get_pid, _from, ev_list) do
    {:reply, self(), ev_list}
  end

  def handle_info(event, ev_list) do
    IO.inspect event
    {:noreply, [event | ev_list]}
  end

end
